package net.sharemycode.security.model;

import org.picketlink.idm.model.AbstractIdentityType;
import org.picketlink.idm.model.Account;
import org.picketlink.idm.model.annotation.AttributeProperty;
import org.picketlink.idm.model.annotation.IdentityStereotype;
import org.picketlink.idm.model.annotation.IdentityStereotype.Stereotype;
import org.picketlink.idm.model.annotation.StereotypeProperty;
import org.picketlink.idm.model.annotation.StereotypeProperty.Property;
import org.picketlink.idm.model.annotation.Unique;
import org.picketlink.idm.query.QueryParameter;

/**
 * Represents the user object
 *
 * @author Shane Bryzak
 */
@IdentityStereotype(Stereotype.USER)
public class User extends AbstractIdentityType implements Account
{
   private static final long serialVersionUID = 8790565692762081434L;
   
   /* Define the query parameters for users */
   public static final QueryParameter USERNAME = QUERY_ATTRIBUTE.byName("username");
   public static final QueryParameter EMAIL = QUERY_ATTRIBUTE.byName("email");
   
   @Unique
   @AttributeProperty
   @StereotypeProperty(Property.IDENTITY_USER_NAME)
   private String username;

   @AttributeProperty
   private String email;

   @AttributeProperty
   private String firstName;

   @AttributeProperty
   private String lastName;

   public String getUsername()
   {
      return username;
   }

   public void setUsername(String username)
   {
      this.username = username;
   }

   public String getEmail()
   {
     return email;
   }

   public void setEmail(String email)
   {
       this.email = email;
   }

   public String getFirstName()
   {
      return firstName;
   }

   public void setFirstName(String firstName)
   {
      this.firstName = firstName;
   }

   public String getLastName()
   {
      return lastName;
   }

   public void setLastName(String lastName)
   {
      this.lastName = lastName;
   }
}
