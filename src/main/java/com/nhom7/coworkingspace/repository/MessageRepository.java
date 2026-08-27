package com.nhom7.coworkingspace.repository;

import com.nhom7.coworkingspace.entity.Message;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @EntityGraph(attributePaths = {"sender", "receiver"})
    @Query("""
            SELECT message
            FROM Message message
            WHERE (message.sender.id = :firstUserId AND message.receiver.id = :secondUserId)
               OR (message.sender.id = :secondUserId AND message.receiver.id = :firstUserId)
            ORDER BY message.createdAt ASC, message.id ASC
            """)
    List<Message> findConversation(
            @Param("firstUserId") Long firstUserId,
            @Param("secondUserId") Long secondUserId);
}
