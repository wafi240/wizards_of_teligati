package com.teligati.app.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.teligati.app.data.entity.UserEntity;
import java.util.List;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(UserEntity user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUsers(List<UserEntity> users);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    UserEntity getUserById(String id);

    @Query("SELECT * FROM users WHERE role = :role")
    List<UserEntity> getUsersByRole(String role);

    @Query("UPDATE users SET availableBalance = :available, escrowBalance = :escrow WHERE id = :userId")
    void updateBalance(String userId, double available, double escrow);

    @Update
    void updateUser(UserEntity user);
}
