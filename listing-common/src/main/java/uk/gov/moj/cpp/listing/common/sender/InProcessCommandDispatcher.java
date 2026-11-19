package uk.gov.moj.cpp.listing.common.sender;

import static javax.ejb.TransactionAttributeType.REQUIRED;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;

import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessorProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;

/**
 * Runs a listing command through the framework's {@code COMMAND_HANDLER} interceptor chain in-VM
 * (MBD refactor P1g), as the handler's {@code listing.handler.command} MDB did.
 *
 * <p>Joins the caller's transaction, or starts one (the command API's REST thread has none). The
 * migrated handlers need a transaction: {@code AggregateStore} locks snapshot rows until commit.
 */
@Stateless
public class InProcessCommandDispatcher {

    static final String COMMAND_HANDLER = "COMMAND_HANDLER";

    @Inject
    private InterceptorChainProcessorProducer interceptorChainProcessorProducer;

    private InterceptorChainProcessor commandHandler;

    @PostConstruct
    void initialise() {
        commandHandler = interceptorChainProcessorProducer.produceLocalProcessor(COMMAND_HANDLER);
    }

    @TransactionAttribute(REQUIRED)
    public void dispatch(final JsonEnvelope command) {
        commandHandler.process(interceptorContextWithInput(command));
    }
}
