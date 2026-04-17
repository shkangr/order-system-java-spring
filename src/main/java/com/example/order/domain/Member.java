package com.example.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(mappedBy = "member")
    private List<Order> orders = new ArrayList<>();

    // === Factory Method === //
    public static Member createMember(String name, String email) {
        Member member = new Member();
        member.name = name;
        member.email = email;
        member.role = Role.USER;
        return member;
    }

    public static Member createMember(String name, String email, String encodedPassword, Role role) {
        Member member = new Member();
        member.name = name;
        member.email = email;
        member.password = encodedPassword;
        member.role = role;
        return member;
    }
}
