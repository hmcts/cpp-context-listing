package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum InitiationCode {
  Q("Q"),

  R("R"),

  S("S"),

  C("C"),

  J("J"),

  Z("Z"),

  O("O");

  private final String value;

  InitiationCode(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<InitiationCode> valueFor(final String value) {
    if(Q.value.equals(value)) { return Optional.of(Q); };
    if(R.value.equals(value)) { return Optional.of(R); };
    if(S.value.equals(value)) { return Optional.of(S); };
    if(C.value.equals(value)) { return Optional.of(C); };
    if(J.value.equals(value)) { return Optional.of(J); };
    if(Z.value.equals(value)) { return Optional.of(Z); };
    if(O.value.equals(value)) { return Optional.of(O); };
    return Optional.empty();
  }
}
