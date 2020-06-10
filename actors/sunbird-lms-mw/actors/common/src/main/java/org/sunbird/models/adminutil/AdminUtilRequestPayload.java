package org.sunbird.models.adminutil;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminUtilRequestPayload implements Serializable {
    private static final long serialVersionUID = -2362783406031347676L;

    @JsonProperty
    private String id;

    @JsonProperty
    private String ver;

    @JsonProperty
    private long ts;

    @JsonProperty
    private AdminUtilParams params;

    @JsonProperty
    private AdminUtilRequest request;

    public AdminUtilRequestPayload() {
    }
    /**
     *
     * @param request
     * @param ver
     * @param id
     * @param params
     * @param ts
     */
    public AdminUtilRequestPayload(String id, String ver, long ts, AdminUtilParams params, AdminUtilRequest request) {
        super();
        this.id = id;
        this.ver = ver;
        this.ts = ts;
        this.params = params;
        this.request = request;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("ver")
    public String getVer() {
        return ver;
    }

    @JsonProperty("ver")
    public void setVer(String ver) {
        this.ver = ver;
    }

    @JsonProperty("ts")
    public long getTs() {
        return ts;
    }

    @JsonProperty("ts")
    public void setTs(long ts) {
        this.ts = ts;
    }

    @JsonProperty("params")
    public AdminUtilParams getParams() {
        return params;
    }

    @JsonProperty("params")
    public void setParams(AdminUtilParams params) {
        this.params = params;
    }

    @JsonProperty("request")
    public AdminUtilRequest getRequest() {
        return request;
    }

    @JsonProperty("request")
    public void setRequest(AdminUtilRequest request) {
        this.request = request;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("id", id).append("ver", ver).append("ts", ts).append("params", params).append("request", request).toString();
    }
}
