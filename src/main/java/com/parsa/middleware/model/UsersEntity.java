package com.parsa.middleware.model;

import javax.persistence.*;
import java.util.Arrays;
import java.util.Objects;

@Entity
@Table(name = "users", schema = "dbo", catalog = "sbomiDB")
public class UsersEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    @Basic
    @Column(name = "encrypted_password")
    private String encryptedPassword;
    @Basic
    @Column(name = "username")
    private String username;
    @Basic
    @Column(name = "salt")
    private byte[] salt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsersEntity that = (UsersEntity) o;
        return id == that.id && Objects.equals(encryptedPassword, that.encryptedPassword) && Objects.equals(username, that.username) && Arrays.equals(salt, that.salt);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, encryptedPassword, username);
        result = 31 * result + Arrays.hashCode(salt);
        return result;
    }
}
