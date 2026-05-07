package com.example.psu.dto.request;

import com.example.psu.enums.PsuTag;
import lombok.Data;

@Data
public class AssignVersionTagRequest {
    private PsuTag tag;
}
