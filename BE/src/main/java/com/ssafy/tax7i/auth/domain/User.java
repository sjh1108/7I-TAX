package com.ssafy.tax7i.auth.domain;

import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ssafyUserId;

    private String email;

    private String name;

    private String ssafyUserKey;

    @Builder
    public User(String ssafyUserId, String email, String name) {
        this.ssafyUserId = ssafyUserId;
        this.email = email;
        this.name = name;
    }

    public void registerFinanceKey(String userKey) {
        this.ssafyUserKey = userKey;
    }
}
