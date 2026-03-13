package com.mitmeerut.CFM_Portal.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Notification", indexes = {
		@Index(name = "idx_note_user_read", columnList = "user_id, is_read"),
		@Index(name = "idx_note_created", columnList = "created_at")
})
public class Notification {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "user_id")
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties("notifications")
	private User user;

	private String type;

	private String title;

	private String message;

	@ManyToOne
	@JoinColumn(name = "sender_id")
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties("notifications")
	private User sender;

	@Column(columnDefinition = "json")
	private String payload; // keep JSON as String for JPA

	@Column(name = "is_read")
	private Boolean isRead = false;

	@Column(name = "created_at")
	private LocalDateTime createdAt = LocalDateTime.now();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public Boolean getIsRead() {
		return isRead;
	}

	public void setIsRead(Boolean isRead) {
		this.isRead = isRead;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public User getSender() {
		return sender;
	}

	public void setSender(User sender) {
		this.sender = sender;
	}

	public Notification(Long id, User user, String type, String payload, Boolean isRead, LocalDateTime createdAt) {
		super();
		this.id = id;
		this.user = user;
		this.type = type;
		this.payload = payload;
		this.isRead = isRead;
		this.createdAt = createdAt;
	}

	public Notification() {
		super();
	}

	@Override
	public String toString() {
		return "Notification [id=" + id + ", user=" + user + ", type=" + type + ", payload=" + payload + ", isRead="
				+ isRead + ", createdAt=" + createdAt + "]";
	}

}
