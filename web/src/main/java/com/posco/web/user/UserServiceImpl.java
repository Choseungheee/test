package com.posco.web.user;

import com.posco.web.auth.*;
import com.posco.web.common.CustomException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Transactional
@Slf4j
@Service
public class UserServiceImpl implements UserService{
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
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
        UserEntity findUser = userRepository.findByEmail(loginDTO.getEmail());//.orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

//        if(!passwordEncoder.matches(loginDTO.getPassword(),findUser.getPassword())){
        System.out.println(loginDTO.getPassword()+" "+findUser.getPassword());
//        if(loginDTO.getPassword().equals(findUser.getPassword())){
//            throw new CustomException(HttpStatus.BAD_REQUEST, "잘못된 비밀번호입니다.");
//        }
        if(tokenRepository.existsByUserId(loginDTO.getEmail())){
            tokenRepository.deleteByUserId(loginDTO.getEmail());
        }
        String accessToken = JwtTokenProvider.makeAccessToken(findUser.toUserDTO());
        String refreshToken = JwtTokenProvider.makeRefreshToken(findUser.getId());
        System.out.println("토큰 생성");
        TokenEntity tokenEntity = TokenEntity.builder()
                .userEntity(findUser)
                .refreshToken(refreshToken)
                .build();
        tokenRepository.save(tokenEntity);
        System.out.println("토큰 저장");
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

    @Override
    public String getId(String email) {
        UserEntity user = userRepository.findByEmail(email);
        return user.getId();
    }

}
