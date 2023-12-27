package com.posco.web.user;

import com.posco.web.auth.LoginDTO;
import com.posco.web.auth.TokenDTO;
import com.posco.web.auth.TokenEntity;
import com.posco.web.auth.TokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService{
    private UserRepository userRepository;
    private TokenRepository tokenRepository;
    private PasswordEncoder passwordEncoder;

    @Override
    public boolean isExistById(String id){
        return userRepository.existsById(id);
    }

    @Override
    public boolean isExistByNickName(String nickName) {
        return userRepository.existsByNickName(nickName);
    }

    @Override
    public boolean isExistByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public UserEntity createUser(UserDTO signUpDTO) {

        UserEntity userEntity = UserEntity.builder()
                .id(signUpDTO.getId())
                .name(signUpDTO.getName())
                .password(passwordEncoder.encode(signUpDTO.getPassword()))
                .email(signUpDTO.getEmail())
                .nickName(signUpDTO.getNickName())
                .level(1L)
                .exp(0L)
                .build();

        return userRepository.save(userEntity);
    }

    @Override
    public UserEntity readUser(String id) {
        return userRepository.findById(id).orElseThrow();
    }

    @Override
    public boolean updateUser(UserDTO userDTO) {
        String id = userDTO.getId();
        UserEntity user = userRepository.findById(id).orElseThrow();
        UserEntity newUser = UserEntity.builder()
                .id(id)
                .nickName(userDTO.getNickName()==null?user.getNickName():userDTO.getNickName())
                .email(user.getEmail())
                .password(userDTO.getPassword()==null? user.getPassword():passwordEncoder.encode(userDTO.getPassword()))
                .profile(userDTO.getProfile()==null? user.getProfile(): userDTO.getProfile())
                .name(user.getName())
                .exp(user.getExp())
                .level(user.getLevel())
                .build();
        UserEntity result = userRepository.save(newUser);
        if(result==null) return false;
        return true;
    }

    @Override
    public boolean checkPassword(String id, String password) {
        UserEntity findUser = userRepository.findById(id).orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if(passwordEncoder.matches(passwordEncoder.encode(password), findUser.getPassword())){
            throw new CustomException(HttpStatus.BAD_REQUEST, "잘못된 비밀번호입니다.");
        }
        return true;
    }

    @Override
    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }

    @Override
    public TokenDTO loginUser(LoginDTO loginDTO) {
        UserEntity findUser = userRepository.findById(loginDTO.getId()).orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if(!passwordEncoder.matches(loginDTO.getPassword(),findUser.getPassword())){
            throw new CustomException(HttpStatus.BAD_REQUEST, "잘못된 비밀번호입니다.");
        }
        if(tokenRepository.existsByUserId(loginDTO.getId())){
            tokenRepository.deleteByUserId(loginDTO.getId());
        }
        String accessToken = JwtTokenProvider.makeAccessToken(findUser.toUserDTO());
        String refreshToken = JwtTokenProvider.makeRefreshToken(findUser.getId());

        TokenEntity tokenEntity = TokenEntity.builder()
                .userEntity(findUser)
                .refreshToken(refreshToken)
                .build();
        tokenRepository.save(tokenEntity);

        return TokenDTO.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .build();
    }

    @Override
    public TokenEntity saveToken(String refreshToken, String userId) {
        TokenEntity findToken = tokenRepository.findByUserId(userId);
//        if(findToken!=null) tokenRepository.deleteById(findToken.getId());
        TokenEntity token = TokenEntity.builder()
                .id(findToken.getId())
                .refreshToken(refreshToken)
                .userEntity(userRepository.findById(userId).orElseThrow())
                .build();
        return tokenRepository.save(token);
    }

    @Override
    public String getToken(String userId) {
        TokenEntity tokenEntity = tokenRepository.findByUserId(userId);
        return tokenEntity.getRefreshToken();
    }

    @Override
    public void deleteToken(String userId) {
        tokenRepository.deleteByUserId(userId);
    }

}
