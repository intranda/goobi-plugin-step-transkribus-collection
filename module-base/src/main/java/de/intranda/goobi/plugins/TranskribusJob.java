package de.intranda.goobi.plugins;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class TranskribusJob {

    @JsonProperty("jobId")
    public String jobId;
    @JsonProperty("docId")
    public Integer docId;
    @JsonProperty("pageNr")
    public Integer pageNr;
    @JsonProperty("type")
    public String type;
    @JsonProperty("state")
    public String state;
    @JsonProperty("success")
    public Boolean success;
    @JsonProperty("description")
    public String description;
    @JsonProperty("userName")
    public String userName;
    @JsonProperty("userId")
    public Integer userId;
    @JsonProperty("createTime")
    public Long createTime;
    @JsonProperty("startTime")
    public Long startTime;
    @JsonProperty("endTime")
    public Long endTime;
    @JsonProperty("jobData")
    public String jobData;
    @JsonProperty("resumable")
    public Boolean resumable;
    @JsonProperty("jobImpl")
    public String jobImpl;
    @JsonProperty("moduleUrl")
    public String moduleUrl;
    @JsonProperty("moduleName")
    public String moduleName;
    @JsonProperty("moduleVersion")
    public String moduleVersion;
    @JsonProperty("started")
    public String started;
    @JsonProperty("ended")
    public String ended;
    @JsonProperty("created")
    public String created;
    @JsonProperty("batchId")
    public Integer batchId;
    @JsonProperty("pageid")
    public Integer pageid;
    @JsonProperty("tsid")
    public Integer tsid;
    @JsonProperty("parent_jobid")
    public Integer parentJobid;
    @JsonProperty("parent_batchid")
    public Integer parentBatchid;
    @JsonProperty("colId")
    public Integer colId;
    @JsonProperty("progress")
    public Integer progress;
    @JsonProperty("totalWork")
    public Integer totalWork;
    @JsonProperty("nrOfErrors")
    public Integer nrOfErrors;
    @JsonProperty("nrInQueue")
    public Integer nrInQueue;
    @JsonProperty("docTitle")
    public String docTitle;
    @JsonProperty("priority")
    public Integer priority;

}
