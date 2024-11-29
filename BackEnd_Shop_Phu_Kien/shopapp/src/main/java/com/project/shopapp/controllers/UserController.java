package com.project.shopapp.controllers;
import java.util.List;
import com.project.shopapp.dtos.requests.UserDTO;
import com.project.shopapp.dtos.requests.UserLoginDTO;
import com.project.shopapp.dtos.responses.user.LoginResponse;
import com.project.shopapp.dtos.responses.user.RegisterResponse;
import com.project.shopapp.models.User;
import com.project.shopapp.services.user.IUserService;
import com.project.shopapp.components.LocalizationUtils;
import com.project.shopapp.utils.MessageKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("${api.prefix}/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;
    private final LocalizationUtils localizationUtils;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> createUser(@Valid @RequestBody UserDTO userDTO, BindingResult bindingResult){
        try {
            if (bindingResult.hasErrors()) {
                List<String> errorMessages = bindingResult.getFieldErrors().stream().map(FieldError::getDefaultMessage).toList();
                return ResponseEntity.badRequest().body(
                        RegisterResponse.builder().message(localizationUtils.getLocalizedMessage(MessageKeys.REGISTER_FAILED, errorMessages)).build()
                );
            }
            if(!userDTO.getPassWord().equals(userDTO.getRetypePassword())){
                return ResponseEntity.badRequest().body(
                        RegisterResponse.builder().message(localizationUtils.getLocalizedMessage(localizationUtils.getLocalizedMessage(MessageKeys.REGISTER_FAILED))).build()
                );
            }
            User user = userService.createUser(userDTO);
            return ResponseEntity.ok(RegisterResponse.builder().message(localizationUtils.getLocalizedMessage(MessageKeys.REGISTER_SUCCESSFULLY)).user(user).build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(RegisterResponse.builder().message(localizationUtils.getLocalizedMessage(MessageKeys.REGISTER_FAILED, e.getMessage())).build());
        }
    }

        @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@Valid @RequestBody UserLoginDTO userLoginDTO){
        try {
            String token = userService.login(userLoginDTO.getPhoneNumber(), userLoginDTO.getPassWord(), userLoginDTO.getRoleId());

            return ResponseEntity.ok(LoginResponse.builder().message(localizationUtils.getLocalizedMessage(MessageKeys.LOGIN_SUCCESSFULLY)).token(token).build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(LoginResponse.builder().message(localizationUtils.getLocalizedMessage(MessageKeys.LOGIN_FAILED, e.getMessage())).build());
        }
    }

}
