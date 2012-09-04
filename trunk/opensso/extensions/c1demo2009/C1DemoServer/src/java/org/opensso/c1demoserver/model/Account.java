/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: Account.java,v 1.2 2009/06/11 05:29:42 superpat7 Exp $
 */

package org.opensso.c1demoserver.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "account")
@NamedQueries({@NamedQuery(name = "Account.findAll", query = "SELECT a FROM Account a"), @NamedQuery(name = "Account.findByAccountNumber", query = "SELECT a FROM Account a WHERE a.accountNumber = :accountNumber"), @NamedQuery(name = "Account.findByBillToAddressLine1", query = "SELECT a FROM Account a WHERE a.billToAddressLine1 = :billToAddressLine1"), @NamedQuery(name = "Account.findByBillToAddressLine2", query = "SELECT a FROM Account a WHERE a.billToAddressLine2 = :billToAddressLine2"), @NamedQuery(name = "Account.findByBillToCity", query = "SELECT a FROM Account a WHERE a.billToCity = :billToCity"), @NamedQuery(name = "Account.findByBillToState", query = "SELECT a FROM Account a WHERE a.billToState = :billToState"), @NamedQuery(name = "Account.findByBillToZip", query = "SELECT a FROM Account a WHERE a.billToZip = :billToZip"), @NamedQuery(name = "Account.findByCreditCardNumber", query = "SELECT a FROM Account a WHERE a.creditCardNumber = :creditCardNumber"), @NamedQuery(name = "Account.findByCvv", query = "SELECT a FROM Account a WHERE a.cvv = :cvv"), @NamedQuery(name = "Account.findByPlanMinutes", query = "SELECT a FROM Account a WHERE a.planMinutes = :planMinutes"), @NamedQuery(name = "Account.findByPlanId", query = "SELECT a FROM Account a WHERE a.planId = :planId"), @NamedQuery(name = "Account.findByChallengeQuestion", query = "SELECT a FROM Account a WHERE a.challengeQuestion = :challengeQuestion"), @NamedQuery(name = "Account.findByChallengeAnswer", query = "SELECT a FROM Account a WHERE a.challengeAnswer = :challengeAnswer")})
public class Account implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "account_number")
    private String accountNumber;
    @Basic(optional = false)
    @Column(name = "bill_to_address_line_1")
    private String billToAddressLine1;
    @Column(name = "bill_to_address_line_2")
    private String billToAddressLine2;
    @Basic(optional = false)
    @Column(name = "bill_to_city")
    private String billToCity;
    @Basic(optional = false)
    @Column(name = "bill_to_state")
    private String billToState;
    @Basic(optional = false)
    @Column(name = "bill_to_zip")
    private String billToZip;
    @Basic(optional = false)
    @Column(name = "credit_card_number")
    private long creditCardNumber;
    @Basic(optional = false)
    @Column(name = "cvv")
    private short cvv;
    @Column(name = "plan_minutes")
    private Integer planMinutes;
    @Column(name = "plan_id")
    private Integer planId;
    @Basic(optional = false)
    @Column(name = "challenge_question")
    private String challengeQuestion;
    @Basic(optional = false)
    @Column(name = "challenge_answer")
    private String challengeAnswer;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "accountNumber")
    private Collection<Phone> phoneCollection;
    @Transient
    private Collection<Question> questionCollection;

    public Account() {
    }

    public Account(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Account(String accountNumber, String billToAddressLine1, String billToCity, String billToState, String billToZip, long creditCardNumber, short cvv, String challengeQuestion, String challengeAnswer) {
        this.accountNumber = accountNumber;
        this.billToAddressLine1 = billToAddressLine1;
        this.billToCity = billToCity;
        this.billToState = billToState;
        this.billToZip = billToZip;
        this.creditCardNumber = creditCardNumber;
        this.cvv = cvv;
        this.challengeQuestion = challengeQuestion;
        this.challengeAnswer = challengeAnswer;

        loadQuestionCollection();
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getBillToAddressLine1() {
        return billToAddressLine1;
    }

    public void setBillToAddressLine1(String billToAddressLine1) {
        this.billToAddressLine1 = billToAddressLine1;
    }

    public String getBillToAddressLine2() {
        return billToAddressLine2;
    }

    public void setBillToAddressLine2(String billToAddressLine2) {
        this.billToAddressLine2 = billToAddressLine2;
    }

    public String getBillToCity() {
        return billToCity;
    }

    public void setBillToCity(String billToCity) {
        this.billToCity = billToCity;
    }

    public String getBillToState() {
        return billToState;
    }

    public void setBillToState(String billToState) {
        this.billToState = billToState;
    }

    public String getBillToZip() {
        return billToZip;
    }

    public void setBillToZip(String billToZip) {
        this.billToZip = billToZip;
    }

    public long getCreditCardNumber() {
        return creditCardNumber;
    }

    public void setCreditCardNumber(long creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
    }

    public short getCvv() {
        return cvv;
    }

    public void setCvv(short cvv) {
        this.cvv = cvv;
    }

    public Integer getPlanMinutes() {
        return planMinutes;
    }

    public void setPlanMinutes(Integer planMinutes) {
        this.planMinutes = planMinutes;
    }

    public Integer getPlanId() {
        return planId;
    }

    public void setPlanId(Integer planId) {
        this.planId = planId;
    }

    public String getChallengeQuestion() {
        return challengeQuestion;
    }

    public void setChallengeQuestion(String challengeQuestion) {
        this.challengeQuestion = challengeQuestion;
    }

    public String getChallengeAnswer() {
        return challengeAnswer;
    }

    public void setChallengeAnswer(String challengeAnswer) {
        this.challengeAnswer = challengeAnswer;
    }

    @PostLoad
    private void loadQuestionCollection() {
        questionCollection = new ArrayList<Question>();
        if ( challengeQuestion != null ) {
            questionCollection.add(new Question(challengeQuestion));
        } else {
            questionCollection.add(new Question("Enter the last 4 digits of the credit card number you used to open the account"));
            questionCollection.add(new Question("Enter the CVV security code of the credit card you used to open the account"));
        }
    }

    public Collection<Question> getQuestionCollection() {
        return questionCollection;
    }

    public Collection<Phone> getPhoneCollection() {
        return phoneCollection;
    }

    public void setPhoneCollection(Collection<Phone> phoneCollection) {
        this.phoneCollection = phoneCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (accountNumber != null ? accountNumber.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Account)) {
            return false;
        }
        Account other = (Account) object;
        if ((this.accountNumber == null && other.accountNumber != null) || (this.accountNumber != null && !this.accountNumber.equals(other.accountNumber))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.opensso.c1demoserver.model.Account[accountNumber=" + accountNumber + "]";
    }

}
