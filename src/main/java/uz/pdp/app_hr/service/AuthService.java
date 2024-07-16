package uz.pdp.app_hr.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import uz.pdp.app_hr.dto.LoginDTO;
import uz.pdp.app_hr.dto.RegisterDTO;
import uz.pdp.app_hr.models.User;
import uz.pdp.app_hr.models.enums.RoleName;
import uz.pdp.app_hr.payload.ApiResponse;
import uz.pdp.app_hr.payload.JwtAuthenticationResponse;
import uz.pdp.app_hr.repository.RoleRepository;
import uz.pdp.app_hr.repository.UserRepository;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final JwtService jwtService;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       RoleRepository roleRepository, JavaMailSender javaMailSender,
                       TemplateEngine templateEngine, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
        this.jwtService = jwtService;
    }

    public ApiResponse registerUser(RegisterDTO registerDTO) {
        boolean emailExists = userRepository.existsByEmail(registerDTO.getEmail());
        if (emailExists) {
            return new ApiResponse("This email has already been registered", false);
        }

        User user = new User();
        user.setFirst_name(registerDTO.getFirst_name());
        user.setLast_name(registerDTO.getLast_name());
        user.setEmail(registerDTO.getEmail());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRoles(Collections.singleton(roleRepository.findByRoleName(RoleName.ROLE_DIRECTOR)));
        user.setEmailCode(UUID.randomUUID().toString());
        userRepository.save(user);


        sendEmail(user.getEmail(), user.getEmailCode());
        return new ApiResponse("Account successfully registered", true);
    }

    public void sendEmail(String sendingEmail, String emailCode) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setFrom("alizanovmarat1@gmail.com");
            helper.setTo(sendingEmail);
            helper.setSubject("Verify your account");

            Context context = new Context();
            context.setVariable("emailCode", emailCode);
            context.setVariable("sendingEmail", sendingEmail);

            String emailContent = templateEngine.process("verify-email", context);

            helper.setText(emailContent, true);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException ignored) {
        }
    }

    public ApiResponse verifyEmail(String email, String emailCode) {
        Optional<User> optionalUser = userRepository.findByEmailAndEmailCode(email, emailCode);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setEnabled(true);
            user.setEmailCode(null);
            userRepository.save(user);
            return new ApiResponse("Email account successfully verified", true);
        }
        return new ApiResponse("Failed to verify email account", false);
    }

    public ApiResponse login(LoginDTO loginDTO) {
        Optional<User> optionalUser = userRepository.findByEmail(loginDTO.getEmail());
        if (optionalUser.isEmpty()) {
            return new ApiResponse("Invalid email or password", false);
        }
        User user = optionalUser.get();
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            return new ApiResponse("Invalid email or password", false);
        }
        String token = jwtService.generateToken(user, user.getRoles());
        return new ApiResponse("Login successful", true, new JwtAuthenticationResponse(token));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(username + " not found"));
    }
}

