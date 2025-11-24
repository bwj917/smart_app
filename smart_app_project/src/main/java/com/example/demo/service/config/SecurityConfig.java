package com.example.demo.service.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt 해싱 알고리즘을 사용하는 PasswordEncoder를 반환
        return new BCryptPasswordEncoder();
    }
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    // CSRF(Cross-Site Request Forgery) 보호 기능을 비활성화합니다.
                    // 모든 접근을 허용하는 경우, CSRF 보호의 의미가 없으므로 비활성화하는 것이 일반적입니다.
                    .csrf(csrf -> csrf.disable())

                    // HTTP 요청에 대한 인가(Authorization) 규칙을 설정합니다.
                    .authorizeHttpRequests(auth -> auth
                            // "/**"는 프로젝트의 모든 URL 경로를 의미합니다.
                            // 모든 경로에 대한 요청을 인증 절차 없이 항상 허용합니다.
                            .requestMatchers("/**").permitAll()
                    );

            // 모든 접근이 허용되므로, 별도의 로그인(formLogin)이나 로그아웃(logout) 설정이 필요하지 않습니다.
            // 따라서 해당 설정을 제거하여 코드를 간결하게 만듭니다.

            return http.build();
        }

}