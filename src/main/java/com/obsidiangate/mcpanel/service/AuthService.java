package com.obsidiangate.mcpanel.service;

import com.obsidiangate.mcpanel.config.AppConfig;
import com.obsidiangate.mcpanel.config.ServerConfig;
import com.obsidiangate.mcpanel.dto.UserDTO;
import com.obsidiangate.mcpanel.model.ServerConfigModel;
import com.obsidiangate.mcpanel.model.UserAuth;
import com.obsidiangate.mcpanel.repository.ServerConfigRepository;
import com.obsidiangate.mcpanel.repository.UserAuthRepository;
import com.obsidiangate.mcpanel.util.enumerator.LogEntryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private LogService logService;

    @Autowired
    private UserAuthRepository userRepository;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private ServerConfigRepository serverConfigRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public boolean authenticate(String token) {

        Optional<UserAuth> userOpt = userRepository.findByToken(token);

        if (userOpt.isPresent()) {
            UserAuth user = userOpt.get();
            // Check if the token is still valid based on last connection time and authTimeMins
            long minutesSinceLastConn = Duration.between(user.getLastConn(), LocalDateTime.now()).toMinutes();
            if (minutesSinceLastConn <= appConfig.getAuthTimeMins()) {
                return true;
            }
        }

        return false;

    }

    public String register(UserDTO userDto){

        if (userRepository.findByUsername(userDto.getUsername()).isPresent() || !serverConfig.isFirstSetup() || userDto.getPassword().length() < 8) {
            return "";
        }

        UserAuth user = new UserAuth();
        user.setUsername(userDto.getUsername());
        String encodedPassword = passwordEncoder.encode(userDto.getPassword());
        user.setPassword(encodedPassword);
        user.setToken(UUID.randomUUID().toString());
        user.setLastConn(LocalDateTime.now());
        user.setAdmin(true);

        userRepository.save(user);

        // You can only create one user, after that the first setup is done and the flag is set to false, preventing new users from being created
        ServerConfigModel config = serverConfigRepository.findActiveConfig();
        config.setFirstSetup(false);
        serverConfigRepository.save(config);
        serverConfig.refresh();

        logService.registerEntry(user.getToken(), "Admin user created: " + user.getUsername(), LogEntryType.AUTHORIZATION);

        return user.getToken();
    }

    public String login(UserDTO userDTO){

        Optional<UserAuth> userOpt = userRepository.findByUsername(userDTO.getUsername());

        if (userOpt.isPresent() && passwordEncoder.matches(userDTO.getPassword(), userOpt.get().getPassword())) {
            UserAuth user = userOpt.get();

            user.setLastConn(LocalDateTime.now());
            user.setToken(UUID.randomUUID().toString());
            userRepository.save(user);

            logService.registerEntry(user.getToken(), user.getUsername() + " loged in.", LogEntryType.AUTHORIZATION);

            return user.getToken();
        }

        return "";

    }

    public boolean createManager(UserDTO userDto, String token) {

        if (userRepository.findByUsername(userDto.getUsername()).isPresent() || userDto.getPassword().length() < 8) {
            return false;
        }

        if (!isAdmin(token)) {
            throw new RuntimeException("Not authorized to create this user");
        }

        UserAuth user = new UserAuth();
        user.setUsername(userDto.getUsername());
        String encodedPassword = passwordEncoder.encode(userDto.getPassword());
        user.setPassword(encodedPassword);
        user.setAdmin(userDto.isAdmin());
        user.setToken(UUID.randomUUID().toString());
        user.setLastConn(LocalDateTime.now());

        userRepository.save(user);

        logService.registerEntry(token, "Manager " + user.getUsername() + " created.", LogEntryType.AUTHORIZATION);

        return true;
    }

    public void deleteManager(String username, String token) {
        UserAuth user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!isAdmin(token)) {
            throw new RuntimeException("Not authorized to delete this user");
        }

        logService.registerEntry(token, "Manager " + user.getUsername() + " deleted.", LogEntryType.AUTHORIZATION);

        userRepository.delete(user);
    }

    public void changePassword(UserDTO user, String token) {

        UserAuth userAuth = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!isAdmin(token)) {
            throw new RuntimeException("Not authorized to change this user's password");
        }

        String encodedPassword = passwordEncoder.encode(user.getPassword());
        userAuth.setPassword(encodedPassword);

        userRepository.save(userAuth);
    }

    public boolean isAdmin(String token){
        Optional<UserAuth> author = userRepository.findByToken(token);
        return author.map(UserAuth::isAdmin).orElse(false);
    }


    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(user -> !"SYSTEM".equals(user.getUsername()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private UserDTO convertToDTO(UserAuth user) {
        UserDTO dto = new UserDTO();
        dto.setUsername(user.getUsername());
        dto.setPassword(null);
        dto.setToken(null);
        dto.setAdmin(user.isAdmin());
        dto.setLastConn(user.getLastConn());
        return dto;
    }
}
