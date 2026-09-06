package com.sami.app.contact.service;
import java.util.Locale;
import java.util.Optional;
/** Pure matching policy: exact approved canonical identifiers only. */
public final class ContactIdentityMatcher {
 private ContactIdentityMatcher(){}
 public record Identity(Long tenantId,String nationalCode,String legalIdentifier,String taxNumber){}
 public enum Match { EXACT_NATIONAL_CODE, EXACT_LEGAL_IDENTIFIER, EXACT_TAX_NUMBER, CANDIDATE_ONLY }
 public static Match classify(Identity left,Identity right) {
  if(left==null||right==null||left.tenantId()==null||!left.tenantId().equals(right.tenantId())) return Match.CANDIDATE_ONLY;
  if(equal(left.nationalCode(),right.nationalCode())) return Match.EXACT_NATIONAL_CODE;
  if(equal(left.legalIdentifier(),right.legalIdentifier())) return Match.EXACT_LEGAL_IDENTIFIER;
  if(equal(left.taxNumber(),right.taxNumber())) return Match.EXACT_TAX_NUMBER;
  return Match.CANDIDATE_ONLY;
 }
 private static boolean equal(String left,String right){return normalized(left).isPresent()&&normalized(left).equals(normalized(right));}
 private static Optional<String> normalized(String value){return value==null?Optional.empty():Optional.of(value.trim().toUpperCase(Locale.ROOT)).filter(v->!v.isEmpty());}
}
