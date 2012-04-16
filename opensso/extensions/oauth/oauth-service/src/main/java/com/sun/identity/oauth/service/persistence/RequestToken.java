/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.oauth.service.persistence;

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
@Table(name = "REQTOKEN")
@NamedQueries({@NamedQuery(name = "RequestToken.findAll", query = "SELECT r FROM RequestToken r"), @NamedQuery(name = "RequestToken.findById", query = "SELECT r FROM RequestToken r WHERE r.id = :id"), @NamedQuery(name = "RequestToken.findByReqtUri", query = "SELECT r FROM RequestToken r WHERE r.reqtUri = :reqtUri"), @NamedQuery(name = "RequestToken.findByReqtVal", query = "SELECT r FROM RequestToken r WHERE r.reqtVal = :reqtVal"), @NamedQuery(name = "RequestToken.findByReqtSecret", query = "SELECT r FROM RequestToken r WHERE r.reqtSecret = :reqtSecret"), @NamedQuery(name = "RequestToken.findByReqtPpalid", query = "SELECT r FROM RequestToken r WHERE r.reqtPpalid = :reqtPpalid"), @NamedQuery(name = "RequestToken.findByReqtLifetime", query = "SELECT r FROM RequestToken r WHERE r.reqtLifetime = :reqtLifetime")})
public class RequestToken implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ID")
    private Integer id;
    @Column(name = "REQT_URI")
    private String reqtUri;
    @Column(name = "REQT_VAL")
    private String reqtVal;
    @Column(name = "REQT_SECRET")
    private String reqtSecret;
    @Column(name = "REQT_PPALID")
    private String reqtPpalid;
    @Column(name = "REQT_LIFETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date reqtLifetime;
    @JoinColumn(name = "CONSUMER_ID", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    private Consumer consumerId;

    public RequestToken() {
    }

    public RequestToken(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getReqtUri() {
        return reqtUri;
    }

    public void setReqtUri(String reqtUri) {
        this.reqtUri = reqtUri;
    }

    public String getReqtVal() {
        return reqtVal;
    }

    public void setReqtVal(String reqtVal) {
        this.reqtVal = reqtVal;
    }

    public String getReqtSecret() {
        return reqtSecret;
    }

    public void setReqtSecret(String reqtSecret) {
        this.reqtSecret = reqtSecret;
    }

    public String getReqtPpalid() {
        return reqtPpalid;
    }

    public void setReqtPpalid(String reqtPpalid) {
        this.reqtPpalid = reqtPpalid;
    }

    public Date getReqtLifetime() {
        return reqtLifetime;
    }

    public void setReqtLifetime(Date reqtLifetime) {
        this.reqtLifetime = reqtLifetime;
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
        if (!(object instanceof RequestToken)) {
            return false;
        }
        RequestToken other = (RequestToken) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.sun.oauth.persistence.RequestToken[id=" + id + "]";
    }

}
