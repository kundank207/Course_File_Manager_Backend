package com.mitmeerut.CFM_Portal.dto;
public class UserApprovalDto {
    private Long userId;
    private Boolean approve;
    private String comment;
    // getters/setters
    public Long getUserId(){return userId;} public void setUserId(Long userId){this.userId=userId;}
    public Boolean getApprove(){return approve;} public void setApprove(Boolean approve){this.approve=approve;}
    public String getComment(){return comment;} public void setComment(String comment){this.comment=comment;}
}

