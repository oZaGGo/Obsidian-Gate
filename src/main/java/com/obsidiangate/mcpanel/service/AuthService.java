package com.obsidiangate.mcpanel.service;

import com.obsidiangate.mcpanel.config.ServerAuthConfig;
import com.obsidiangate.mcpanel.config.ServerConfig;
import com.obsidiangate.mcpanel.dto.UserDTO;
import com.obsidiangate.mcpanel.model.ServerConfigModel;
import com.obsidiangate.mcpanel.model.UserAuth;
import com.obsidiangate.mcpanel.repository.ServerConfigRepository;
import com.obsidiangate.mcpanel.repository.UserAuthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserAuthRepository userRepository;

    @Autowired
    private ServerAuthConfig authConfig;

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private ServerConfigRepository serverConfigRepository;

    public boolean authenticate(String token) {

        Optional<UserAuth> userOpt = userRepository.findByToken(token);

        if (userOpt.isPresent()) {
            UserAuth user = userOpt.get();
            // Check if the token is still valid based on last connection time and authTimeMins
            long minutesSinceLastConn = Duration.between(user.getLastConn(), LocalDateTime.now()).toMinutes();
            if (minutesSinceLastConn <= authConfig.getAuthTimeMins()) {
                return true;
            }
        }

        return false;

    }

    public String register(UserDTO userDto){

        if (userRepository.findByUsername(userDto.getUsername()).isPresent() || !serverConfig.isFirstSetup()) {
            return "";
        }

        UserAuth user = new UserAuth();
        user.setUsername(userDto.getUsername());
        user.setPassword(userDto.getPassword());
        user.setToken(UUID.randomUUID().toString());
        user.setLastConn(LocalDateTime.now());

        userRepository.save(user);

        // You can only create one user, after that the first setup is done and the flag is set to false, preventing new users from being created
        ServerConfigModel config = serverConfigRepository.findActiveConfig();
        config.setFirstSetup(false);
        serverConfigRepository.save(config);

        return user.getToken();
    }

    public String login(UserDTO userDTO){

        Optional<UserAuth> userOpt = userRepository.findByUsername(userDTO.getUsername());

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(userDTO.getPassword())) {
            UserAuth user = userOpt.get();

            user.setLastConn(LocalDateTime.now());
            user.setToken(UUID.randomUUID().toString());
            userRepository.save(user);
            return user.getToken();
        }

        return "";

    }
}
