package com.upgrad.quora.service.business;

import com.upgrad.quora.service.dao.QuestionDao;
import com.upgrad.quora.service.dao.UserDao;
import com.upgrad.quora.service.entity.QuestionEntity;
import com.upgrad.quora.service.entity.UserAuthTokenEntity;
import com.upgrad.quora.service.entity.UserEntity;
import com.upgrad.quora.service.exception.AuthorizationFailedException;
import com.upgrad.quora.service.exception.InvalidQuestionException;
import com.upgrad.quora.service.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuestionBusinessService {
    @Autowired
    private UserDao userDao;

    @Autowired
    private QuestionDao questionDao;

    @Transactional(propagation = Propagation.REQUIRED)
    public QuestionEntity createQuestion(QuestionEntity questionEntity, String accessToken) throws AuthorizationFailedException {
        UserAuthTokenEntity userAuthEntity = userDao.getUserAuthToken(accessToken);
        authorizeUser(userAuthEntity, "User is signed out.Sign in first to post a question");

        questionEntity.setUser(userAuthEntity.getUser());
        return questionDao.createQuestion(questionEntity);
    }
    @Transactional(propagation = Propagation.REQUIRED)
    public QuestionEntity editQuestion(final QuestionEntity questionEntity, final String authorizationToken) throws AuthorizationFailedException, InvalidQuestionException {
        UserAuthTokenEntity userAuthEntity = userDao.getUserAuthToken(authorizationToken);

        authorizeUser(userAuthEntity, "User is signed out.Sign in first to edit the question");

        // Validate if requested question exist or not
        QuestionEntity existingQuestionEntity = questionDao.getQuestionById(questionEntity.getUuid());
        if (existingQuestionEntity == null) {
            throw new InvalidQuestionException("QUES-001", "Entered question uuid does not exist");
        }
        // Validate if current user is the owner of requested question
        UserEntity currentUser = userAuthEntity.getUser();
        UserEntity questionOwner = existingQuestionEntity.getUser();
        if (currentUser.getId() != questionOwner.getId()) {
            throw new AuthorizationFailedException("ATHR-003", "Only the question owner can edit the question");
        }
        questionEntity.setId(existingQuestionEntity.getId());
        questionEntity.setUser(existingQuestionEntity.getUser());
        questionEntity.setDate(existingQuestionEntity.getDate());

        return questionDao.updateQuestion(questionEntity);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteQuestion(final String questionId, final String authorization) throws InvalidQuestionException, AuthorizationFailedException {
        UserAuthTokenEntity userAuthEntity = userDao.getUserAuthToken(authorization);

        authorizeUser(userAuthEntity, "User is signed out.Sign in first to delete a question");
        QuestionEntity questionEntity = questionDao.getQuestionById(questionId);
        // Validate if requested question exist or not
        if (questionEntity == null) {
            throw new InvalidQuestionException("QUES-001", "Entered question uuid does not exist");
        }
        // Validate if current user is the owner of requested question or the role of user is admin
        if (userAuthEntity.getUser().getRole().equals("nonadmin") && userAuthEntity.getUser().getUuid() != questionEntity.getUser().getUuid()) {
            throw new AuthorizationFailedException("ATHR-003", "Only the question owner or admin can delete the question");
        }

        questionDao.deleteQuestion(questionEntity);
    }

    private void authorizeUser(UserAuthTokenEntity userAuthEntity, final String log_out_ERROR) throws AuthorizationFailedException {
        // Validate if user is signed in or not
        if (userAuthEntity == null) {
            throw new AuthorizationFailedException("ATHR-001", "User has not signed in");
        }

        // Validate if user has signed out
        if (userAuthEntity.getLogoutAt() != null) {
            throw new AuthorizationFailedException("ATHR-002", log_out_ERROR);
        }
    }

    public List<QuestionEntity> getAllQuestions(String token) throws AuthorizationFailedException {
        UserAuthTokenEntity userAuthEntity = userDao.getUserAuthToken(token);
        authorizeUser(userAuthEntity, "User is signed out.Sign in first to get all questions");
        return questionDao.getAllQuestions();
    }

    public List<QuestionEntity> getAllQuestionsByUser(final String userId, final String authorizationToken) throws AuthorizationFailedException, UserNotFoundException {
        UserAuthTokenEntity userAuthEntity = userDao.getUserAuthToken(authorizationToken);

        authorizeUser(userAuthEntity, "User is signed out.Sign in first to get all questions posted by a specific user");
        // Validate if requested user exist or not
        if (userDao.getUser(userId) == null) {
            throw new UserNotFoundException("USR-001", "User with entered uuid whose question details are to be seen does not exist");
        }
        return questionDao.getAllQuestionsByUser(userId);
    }
}
