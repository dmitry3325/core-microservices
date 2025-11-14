package com.corems.userms.app.controller;

import com.corems.userms.api.ProfileApi;
import com.corems.userms.api.model.ChangePasswordRequest;
import com.corems.userms.api.model.SuccessfulResponse;
import com.corems.userms.api.model.UserInfo;
import com.corems.userms.api.model.UserProfileUpdateRequest;
import com.corems.userms.app.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequiredArgsConstructor
public class ProfileController implements ProfileApi {

    private final ProfileService profileService;

    @Override
    public ResponseEntity<UserInfo> currentUserInfo() {
        return ResponseEntity.ok(profileService.getCurrentUserInfo());
    }

    @Override
    public ResponseEntity<UserInfo> updateCurrentUserProfile(UserProfileUpdateRequest userProfileUpdateRequest) {
        return ResponseEntity.ok(profileService.updateCurrentUserProfile(userProfileUpdateRequest));
    }

    @Override
    public ResponseEntity<SuccessfulResponse> changeOwnPassword(ChangePasswordRequest changePasswordRequest) {
        return ResponseEntity.ok(profileService.changeOwnPassword(changePasswordRequest));
    }
}
