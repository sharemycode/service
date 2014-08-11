/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sharemycode;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

import org.picketlink.idm.jpa.annotations.AttributeValue;
import org.picketlink.idm.jpa.annotations.entity.IdentityManaged;


@SuppressWarnings("serial")
@Entity
@XmlRootElement
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    @Size(min = 1, max = 25)
    //@Pattern(regexp = "([a-zA-Z]|-)*", message = "Must not contain numbers")
    @Pattern(regexp = "([a-zA-Z]|-)*", message = "Must only contain letters and '-'")
    private String givenName;
    
    @NotNull
    @Size(min = 1, max = 25)
    @Pattern(regexp = "([a-zA-Z]|-)*", message = "Must only contain letters and '-'")
    private String surname;

    @NotNull
    @Size(min=1)
    @Email
    private String email;

    @NotNull
    @Size(min = 8, max=26)
    @Pattern(regexp = "", message = "Must contain a combination of digits, uppercase and lowercase letters and symbols ; : , @ & ! ^")
    private String password;
    
    @NotNull
    @Size(min = 10, max = 12)
    @Digits(fraction = 0, integer = 12)
    @Column(name = "phone_number")
    private String phoneNumber;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String name) {
        this.givenName = name;
    }
    
    public String getSurname() {
        return surname;
    }

    public void setSurname(String name) {
        this.surname = name;
    }
    
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
/*
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
*/
}
/*
	public User(String username, String password, String email, String given, String surname) {
		this.username = username;
		this.password = password;
		this.email = email;
		this.givenName = given;
		this.surname = surname;
		// TODO Add user to resource list
*/