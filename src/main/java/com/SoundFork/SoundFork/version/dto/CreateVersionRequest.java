package com.SoundFork.SoundFork.version.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateVersionRequest {

    @NotBlank(message = "Commit message is required")
    @Size(min = 1, max = 500, message = "Commit message must be between 1 and 500 characters")
    private String commitMessage;
}
