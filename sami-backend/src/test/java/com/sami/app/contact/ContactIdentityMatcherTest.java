package com.sami.app.contact;
import static org.assertj.core.api.Assertions.assertThat;
import com.sami.app.contact.service.ContactIdentityMatcher; import com.sami.app.contact.service.ContactIdentityMatcher.*;
import org.junit.jupiter.api.Test;
class ContactIdentityMatcherTest {
 @Test void exactIdentifierRequiresSameTenant(){assertThat(ContactIdentityMatcher.classify(new Identity(1L,"001",null,null),new Identity(1L,"001",null,null))).isEqualTo(Match.EXACT_NATIONAL_CODE);assertThat(ContactIdentityMatcher.classify(new Identity(1L,"001",null,null),new Identity(2L,"001",null,null))).isEqualTo(Match.CANDIDATE_ONLY);}
 @Test void similarNameIsNeverAnAutomaticMatch(){assertThat(ContactIdentityMatcher.classify(new Identity(1L,null,null,null),new Identity(1L,null,null,null))).isEqualTo(Match.CANDIDATE_ONLY);}
 @Test void exactTaxNumberIsExplicitAndNormalized(){assertThat(ContactIdentityMatcher.classify(new Identity(1L,null,null," ir-9 "),new Identity(1L,null,null,"IR-9"))).isEqualTo(Match.EXACT_TAX_NUMBER);}
}
