/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.oauth.service.persistence;

import com.sun.identity.oauth.service.*;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 */
@Entity
@Table(name = "ACCTOKEN")
@NamedQueries({@NamedQuery(name = "AccessToken.findAll", query = "SELECT a FROM AccessToken a"), @NamedQuery(name = "AccessToken.findById", query = "SELECT a FROM AccessToken a WHERE a.id = :id"), @NamedQuery(name = "AccessToken.findByAcctUri", query = "SELECT a FROM AccessToken a WHERE a.acctUri = :acctUri"), @NamedQuery(name = "AccessToken.findByAcctVal", query = "SELECT a FROM AccessToken a WHERE a.acctVal = :acctVal"), @NamedQuery(name = "AccessToken.findByAcctSecret", query = "SELECT a FROM AccessToken a WHERE a.acctSecret = :acctSecret"), @NamedQuery(name = "AccessToken.findByAcctPpalid", query = "SELECT a FROM AccessToken a WHERE a.acctPpalid = :acctPpalid"), @NamedQuery(name = "AccessToken.findByAcctLifetime", query = "SELECT a FROM AccessToken a WHERE a.acctLifetime = :acctLifetime"), @NamedQuery(name = "AccessToken.findByAcctOnetime", query = "SELECT a FROM AccessToken a WHERE a.acctOnetime = :acctOnetime")})
public class AccessToken implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ID")
    private Integer id;
    @Column(name = "ACCT_URI")
    private String acctUri;
    @Column(name = "ACCT_VAL")
    private String acctVal;
    @Column(name = "ACCT_SECRET")
    private String acctSecret;
    @Column(name = "ACCT_PPALID")
    private String acctPpalid;
    @Column(name = "ACCT_LIFETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date acctLifetime;
    @Column(name = "ACCT_ONETIME")
    private Short acctOnetime;
    @JoinColumn(name = "CONSUMER_ID", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    private Consumer consumerId;

    public AccessToken() {
    }

    public AccessToken(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAcctUri() {
        return acctUri;
    }

    public void setAcctUri(String acctUri) {
        this.acctUri = acctUri;
    }

    public String getAcctVal() {
        return acctVal;
    }

    public void setAcctVal(String acctVal) {
        this.acctVal = acctVal;
    }

    public String getAcctSecret() {
        return acctSecret;
    }

    public void setAcctSecret(String acctSecret) {
        this.acctSecret = acctSecret;
    }

    public String getAcctPpalid() {
        return acctPpalid;
    }

    public void setAcctPpalid(String acctPpalid) {
        this.acctPpalid = acctPpalid;
    }

    public Date getAcctLifetime() {
        return acctLifetime;
    }

    public void setAcctLifetime(Date acctLifetime) {
        this.acctLifetime = acctLifetime;
    }

    public Short getAcctOnetime() {
        return acctOnetime;
    }

    public void setAcctOnetime(Short acctOnetime) {
        this.acctOnetime = acctOnetime;
    }

    public Consumer getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(Consumer consumerId) {
        this.consumerId = consumerId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof AccessToken)) {
            return false;
        }
        AccessToken other = (AccessToken) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.sun.oauth.persistence.AccessToken[id=" + id + "]";
    }

}
