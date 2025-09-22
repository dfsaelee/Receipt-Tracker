package PersonalCPI.PersonalCPI.dto;

import PersonalCPI.PersonalCPI.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponseDto {
    private Long id;
    private String username;
    private String email;
    private boolean enabled;

    public UserResponseDto(Long id, String username, String email, boolean enabled) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.enabled = enabled;
    }

    // Static method to create from User entity
    public static UserResponseDto fromUser(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled()
        );
    }
}