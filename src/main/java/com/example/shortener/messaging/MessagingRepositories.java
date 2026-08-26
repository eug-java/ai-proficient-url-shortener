package com.example.shortener.messaging;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ConsumerInboxRepository extends JpaRepository<ConsumerInbox, UUID> {}
