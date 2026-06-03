package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.LoginFailureReferenceId;

public interface LoginFailureReferenceIdGenerator {

    LoginFailureReferenceId generate();
}
