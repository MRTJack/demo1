package uz.pdp.app_hr.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
    @Size(min = 3, max = 20)
    private String first_name;

    @Size(min = 5, max = 20)
    private String last_name;

    @Email
    @NotNull
    private String email;

    @NotNull
    private String password;
}
