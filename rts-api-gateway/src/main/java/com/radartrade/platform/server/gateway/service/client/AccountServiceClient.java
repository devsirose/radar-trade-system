package com.radartrade.platform.server.gateway.service.client;

import com.radartrade.platform.server.gateway.grpc.AccountServiceGrpc;
import com.radartrade.platform.server.gateway.grpc.CreateAccountRequest;
import com.radartrade.platform.server.gateway.grpc.CreateAccountResponse;
import io.grpc.ManagedChannel;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceClient  {
    private ManagedChannel channel;
    private AccountServiceGrpc.AccountServiceBlockingStub blockingStub;

    public AccountServiceClient(ManagedChannel channel) {
        this.channel = channel;
    }

    @PostConstruct
    public void connect() {
        blockingStub = AccountServiceGrpc.newBlockingStub(channel);
    }

    public CreateAccountResponse createAccount(CreateAccountRequest createAccountRequest) {
        return blockingStub.createAccount(createAccountRequest);
    }
}
