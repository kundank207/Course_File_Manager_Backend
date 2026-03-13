package com.mitmeerut.CFM_Portal.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class HeadingTreeDto {

    private Long id;
    private String title;
    private List<HeadingTreeDto> children = new ArrayList<>();

    public HeadingTreeDto(Long id, String title) {
        this.id = id;
        this.title = title;
    }


}
