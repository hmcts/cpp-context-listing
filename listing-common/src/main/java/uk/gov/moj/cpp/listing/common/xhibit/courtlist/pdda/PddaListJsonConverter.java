package uk.gov.moj.cpp.listing.common.xhibit.courtlist.pdda;

import static java.lang.String.format;

import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.common.xhibit.exception.GenerationFailedException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Converts a generated Xhibit court list document (DailyList, WarnedList or FirmList, as a DOM tree
 * marshalled from the mappers' JAXB objects) into the JSON shape of the matching HMCTS PDDA schema
 * (crown_daily/advance/firm_pdda_list.json).
 * <p>
 * The document is first read into a generic tree (attributes become properties, repeated elements become
 * arrays). That tree is then reshaped by walking the PDDA schema, which decides where arrays are
 * needed, which wrapper elements to unwrap, which values are integers, and which properties are kept.
 */
public class PddaListJsonConverter {

    private static final String TEXT = "#text";
    private static final ZoneId XHIBIT_ZONE = ZoneId.of("Europe/London");
    private static final String SITTING_DATE = "SittingDate";
    private static final String SITTINGS = "Sittings";
    private static final String CASE_NUMBER = "CaseNumber";
    private static final String CASE_NUMBER_CATH = "CaseNumberCaTH";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, JsonNode> schemas = new ConcurrentHashMap<>();

    public JsonNode convert(final Element xmlRoot, final PublishCourtListType publishCourtListType) {
        final JsonNode schema = loadSchema(publishCourtListType);

        final ObjectNode generic = objectMapper.createObjectNode();
        generic.set(localName(xmlRoot), toGenericNode(xmlRoot));
        addCaseNumberCaTH(generic);

        return normaliseObject(generic, schema, schema);
    }

    public static String schemaResource(final PublishCourtListType publishCourtListType) {
        switch (publishCourtListType) {
            case DRAFT:
            case FINAL:
                return "pdda/crown_daily_pdda_list.json";
            case WARN:
                return "pdda/crown_advance_pdda_list.json";
            case FIRM:
                return "pdda/crown_firm_pdda_list.json";
            default:
                throw new GenerationFailedException("No PDDA schema for " + publishCourtListType);
        }
    }

    private JsonNode loadSchema(final PublishCourtListType publishCourtListType) {
        return schemas.computeIfAbsent(schemaResource(publishCourtListType), resource -> {
            try (final InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
                if (in == null) {
                    throw new GenerationFailedException("PDDA schema not found: " + resource);
                }
                return objectMapper.readTree(in);
            } catch (final IOException e) {
                throw new GenerationFailedException("Could not read PDDA schema " + resource, e);
            }
        });
    }

    // ---- XML to generic tree -------------------------------------------------------------

    private JsonNode toGenericNode(final Element element) {
        final ObjectNode node = objectMapper.createObjectNode();

        final NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            final Attr attribute = (Attr) attributes.item(i);
            if (!isNamespaceDeclaration(attribute)) {
                node.put(localName(attribute), attribute.getValue());
            }
        }

        boolean hasChildElements = false;
        final NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                hasChildElements = true;
                final Element child = (Element) children.item(i);
                append(node, localName(child), toGenericNode(child));
            }
        }

        if (hasChildElements) {
            return node;
        }

        final String text = element.getTextContent() == null ? "" : element.getTextContent().trim();
        if (node.size() == 0) {
            return new TextNode(text);
        }
        node.put(TEXT, text);
        return node;
    }

    private void append(final ObjectNode parent, final String name, final JsonNode value) {
        final JsonNode existing = parent.get(name);
        if (existing == null) {
            parent.set(name, value);
        } else if (existing.isArray()) {
            ((ArrayNode) existing).add(value);
        } else {
            final ArrayNode array = objectMapper.createArrayNode();
            array.add(existing);
            array.add(value);
            parent.set(name, array);
        }
    }

    private boolean isNamespaceDeclaration(final Attr attribute) {
        return "xmlns".equals(attribute.getPrefix()) || "xmlns".equals(attribute.getName());
    }

    private String localName(final Node node) {
        return node.getLocalName() != null ? node.getLocalName() : node.getNodeName();
    }

    /**
     * The PDDA schemas require CaseNumberCaTH, which the Xhibit documents do not carry. Until the
     * owning system provides it, the case number is copied (interim decision D2 in plan 002).
     */
    private void addCaseNumberCaTH(final JsonNode node) {
        if (node.isObject()) {
            final ObjectNode object = (ObjectNode) node;
            if (object.has(CASE_NUMBER) && !object.has(CASE_NUMBER_CATH) && object.get(CASE_NUMBER).isTextual()) {
                object.set(CASE_NUMBER_CATH, object.get(CASE_NUMBER));
            }
        }
        node.forEach(this::addCaseNumberCaTH);
    }

    // ---- schema guided reshaping ---------------------------------------------------------

    private JsonNode normalise(final JsonNode node, final JsonNode schema, final JsonNode root) {
        final JsonNode resolved = resolve(schema, node, root);
        switch (resolved.path("type").asText()) {
            case "object":
                return normaliseObject(node, resolved, root);
            case "array":
                return normaliseArray(node, resolved, root);
            case "integer":
                return normaliseInteger(node);
            default:
                return "date-time".equals(resolved.path("format").asText())
                        ? normaliseDateTime(normaliseText(node))
                        : normaliseText(node);
        }
    }

    private JsonNode normaliseObject(final JsonNode node, final JsonNode schema, final JsonNode root) {
        if (!node.isObject()) {
            return node;
        }
        final ObjectNode source = (ObjectNode) node;
        hoistTextAttributes(source);

        final ObjectNode result = objectMapper.createObjectNode();
        final JsonNode properties = schema.path("properties");
        final Iterator<String> names = properties.fieldNames();
        while (names.hasNext()) {
            final String name = names.next();
            final JsonNode child = source.get(name);
            if (child != null) {
                result.set(name, normalise(child, properties.get(name), root));
            }
        }
        return result;
    }

    /**
     * An element with both text and attributes, for example
     * {@code <CourtHouseCode CourtHouseShortName="X">001</CourtHouseCode>}, is read as an object with a
     * text entry. The PDDA schemas hold the value as a plain property and the attribute as a sibling
     * property, so the attributes are lifted to the parent and the text replaces the object.
     */
    private void hoistTextAttributes(final ObjectNode source) {
        final List<String> names = new ArrayList<>();
        source.fieldNames().forEachRemaining(names::add);
        for (final String name : names) {
            final JsonNode value = source.get(name);
            if (value.isObject() && value.has(TEXT)) {
                final Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
                while (fields.hasNext()) {
                    final Map.Entry<String, JsonNode> field = fields.next();
                    if (!TEXT.equals(field.getKey()) && !source.has(field.getKey())) {
                        source.set(field.getKey(), field.getValue());
                    }
                }
                source.set(name, value.get(TEXT));
            }
        }
    }

    private JsonNode normaliseArray(final JsonNode node, final JsonNode schema, final JsonNode root) {
        final JsonNode itemSchema = schema.path("items");
        final JsonNode resolvedItems = resolve(itemSchema, null, root);
        final Set<String> itemProperties = new HashSet<>();
        resolvedItems.path("properties").fieldNames().forEachRemaining(itemProperties::add);

        final List<JsonNode> flattened = new ArrayList<>();
        flatten(node, itemProperties, flattened);

        final ArrayNode result = objectMapper.createArrayNode();
        for (final JsonNode item : flattened) {
            final JsonNode normalised = normalise(item, itemSchema, root);
            if (!isEmptyDatedPlaceholder(normalised, resolvedItems)) {
                result.add(normalised);
            }
        }
        return result;
    }

    /**
     * Wrapper elements such as {@code <Defendants><Defendant>...} read as an object with one key that
     * is not a property of the array item, so the wrapper is replaced by its content. A single
     * element where an array is expected is wrapped into a one item array.
     */
    private void flatten(final JsonNode node, final Set<String> itemProperties, final List<JsonNode> accumulator) {
        if (node.isArray()) {
            node.forEach(element -> flatten(element, itemProperties, accumulator));
        } else if (node.isTextual() && node.asText().isBlank() && !itemProperties.isEmpty()) {
            return; // an empty element such as <Sittings/> stands for "no items"
        } else if (node.isObject() && node.size() == 1 && !itemProperties.isEmpty()
                && !itemProperties.contains(node.fieldNames().next())) {
            flatten(node.elements().next(), itemProperties, accumulator);
        } else {
            accumulator.add(node);
        }
    }

    /**
     * The firm list schema requires a SittingDate on every court list. Xhibit emits a bare court house
     * with an empty Sittings element for court houses that have nothing listed; it has no date and
     * carries nothing beyond the court house, so it is left out rather than given an invented date.
     */
    private boolean isEmptyDatedPlaceholder(final JsonNode item, final JsonNode itemSchema) {
        if (!item.isObject() || item.has(SITTING_DATE)) {
            return false;
        }
        boolean sittingDateRequired = false;
        for (final JsonNode required : itemSchema.path("required")) {
            sittingDateRequired |= SITTING_DATE.equals(required.asText());
        }
        return sittingDateRequired && (!item.has(SITTINGS) || item.get(SITTINGS).size() == 0);
    }

    /**
     * Xhibit date-times are London local time with no offset. The PDDA schemas use the date-time
     * format, which needs an offset, so such values are converted to UTC.
     */
    private JsonNode normaliseDateTime(final JsonNode node) {
        if (!node.isTextual()) {
            return node;
        }
        final String value = node.asText().trim();
        try {
            OffsetDateTime.parse(value);
            return node;
        } catch (final DateTimeParseException notOffset) {
            try {
                return new TextNode(LocalDateTime.parse(value).atZone(XHIBIT_ZONE).toInstant().toString());
            } catch (final DateTimeParseException notLocal) {
                return node;
            }
        }
    }

    private JsonNode normaliseInteger(final JsonNode node) {
        final JsonNode text = node.isObject() && node.has(TEXT) ? node.get(TEXT) : node;
        if (text.isTextual()) {
            try {
                return objectMapper.getNodeFactory().numberNode(Integer.parseInt(text.asText().trim()));
            } catch (final NumberFormatException e) {
                return text;
            }
        }
        return text;
    }

    private JsonNode normaliseText(final JsonNode node) {
        return node.isObject() && node.has(TEXT) ? node.get(TEXT) : node;
    }

    private JsonNode resolve(final JsonNode schema, final JsonNode node, final JsonNode root) {
        JsonNode current = schema;
        while (current.has("$ref")) {
            current = pointer(root, current.get("$ref").asText());
        }
        if (current.has("oneOf") && node != null && node.isObject()) {
            for (final JsonNode branch : current.get("oneOf")) {
                if (hasAllRequired(node, branch)) {
                    return resolve(branch, node, root);
                }
            }
        }
        return current;
    }

    private boolean hasAllRequired(final JsonNode node, final JsonNode branch) {
        for (final JsonNode required : branch.path("required")) {
            if (!node.has(required.asText())) {
                return false;
            }
        }
        return true;
    }

    private JsonNode pointer(final JsonNode root, final String reference) {
        if (!reference.startsWith("#/")) {
            throw new GenerationFailedException(format("Unsupported schema reference %s", reference));
        }
        final JsonNode target = root.at(reference.substring(1));
        if (target.isMissingNode()) {
            throw new GenerationFailedException(format("Schema reference %s not found", reference));
        }
        return target;
    }
}
