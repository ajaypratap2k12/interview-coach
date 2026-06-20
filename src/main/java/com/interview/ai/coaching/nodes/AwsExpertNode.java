package com.interview.ai.coaching.nodes;

import com.interview.ai.coaching.graph.InterviewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

/**
 * AWS expert agent node.
 *
 * <p>Answers ONLY from the AWS domain. Must not mention other technologies.
 * Writes to {@code awsAnswer} and appends {@code "AWS"} to
 * {@code completedAgents}.</p>
 *
 * @author Interview AI Coaching Team
 * @version 2.0
 */
@Slf4j
@RequiredArgsConstructor
public class AwsExpertNode {

    private static final String NODE_NAME = "aws_agent";
    private static final String AGENT_ID = "AWS";

    private final ChatClient chatClient;

    public Map<String, Object> execute(InterviewState state) {
        log.info("[{}] Executing node", NODE_NAME);
        log.info("[{}] Input: question='{}'", NODE_NAME, state.question());

        String answer = chatClient.prompt()
                .user("""
                    You are an AWS Solutions Architect expert. Answer ONLY from the AWS domain.

                    RULES:
                    - Answer exclusively about Amazon Web Services
                    - Do NOT mention Java, Spring, Kafka, microservices patterns, or any other technology
                    - Do NOT provide a complete end-to-end answer that covers multiple domains
                    - Focus only on the AWS-specific parts of the question
                    - If the question has no AWS component, state that briefly

                    AWS topics you cover:
                    EC2, S3, Lambda, IAM, VPC, ECS, EKS, RDS, DynamoDB,
                    SQS, SNS, CloudFormation, API Gateway, CloudWatch

                    The candidate holds an AWS certification.
                    Provide concise, technically accurate answers with architecture guidance.

                    Question: %s
                    """.formatted(state.question()))
                .call()
                .content();

        log.info("[{}] Output: awsAnswer ({} chars)", NODE_NAME, answer.length());
        return Map.of("awsAnswer", answer, "completedAgents", List.of(AGENT_ID));
    }
}
