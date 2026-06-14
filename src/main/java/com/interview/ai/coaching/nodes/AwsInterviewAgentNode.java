package com.interview.ai.coaching.nodes;

import com.interview.ai.coaching.graph.InterviewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

/**
 * Node implementation for AWS-specific interview questions.
 * 
 * This class represents a specialized node in the interview coaching graph
 * that handles AWS-specific questions. It uses Spring AI's {@link ChatClient}
 * to interact with an LLM configured as an AWS Solutions Architect interviewer.
 * 
 * <p>Topics covered:</p>
 * <ul>
 *   <li>EC2 (instance types, placement groups, auto scaling, spot instances)</li>
 *   <li>S3 (storage classes, lifecycle policies, replication, encryption)</li>
 *   <li>IAM (users, groups, roles, policies, federation)</li>
 *   <li>VPC (subnets, route tables, NAT gateways, peering, endpoints)</li>
 *   <li>ECS (task definitions, services, Fargate vs EC2 launch types)</li>
 *   <li>EKS (clusters, node groups, kubernetes networking, IRSA)</li>
 *   <li>Cloud Architecture (well-architected framework, cost optimization, resilience)</li>
 * </ul>
 * 
 * @author Interview AI Coaching Team
 * @version 1.0
 * @see ChatClient
 * @see InterviewState
 * @see org.bsc.langgraph4j.action.NodeAction
 */
@Slf4j
@RequiredArgsConstructor
public class AwsInterviewAgentNode {

    private static final String NODE_NAME = "aws_agent";

    private final ChatClient chatClient;

    /**
     * Executes the AWS interview agent node.
     * 
     * @param state the current interview state containing the question
     * @return a map containing the state updates with the generated answer
     */
    public Map<String, Object> execute(InterviewState state) {
        log.info("[{}] Executing node", NODE_NAME);
        log.info("[{}] Input state: question='{}', category={}, answer='{}'",
                NODE_NAME, state.question(), state.category(), state.answer());

        String question = state.question();

        String answer = chatClient.prompt()
                .user("""
                    You are an AWS Solutions Architect interviewer with 15+ years of cloud experience.
                    
                    Topics you cover:
                    * EC2 (instance types, placement groups, auto scaling, spot instances, AMIs)
                    * S3 (storage classes, lifecycle policies, replication, encryption, versioning)
                    * IAM (users, groups, roles, policies, federation, cross-account access)
                    * VPC (subnets, route tables, NAT gateways, peering, endpoints, security groups)
                    * ECS (task definitions, services, Fargate vs EC2 launch types, load balancing)
                    * EKS (clusters, node groups, kubernetes networking, IRSA, pod security)
                    * Cloud Architecture (well-architected framework, cost optimization, disaster recovery)
                    
                    The candidate holds an AWS certification.
                    Provide concise but technically accurate answers.
                    Include architecture diagrams in text where relevant.
                    Cover best practices and cost implications.
                    
                    Question:
                    %s
                    """.formatted(question))
                .call()
                .content();

        Map<String, Object> outputUpdates = Map.of("answer", answer);
        log.info("[{}] Output updates: answer='{}...'", NODE_NAME, answer.substring(0, Math.min(answer.length(), 100)));
        log.info("[{}] Node execution complete", NODE_NAME);

        return outputUpdates;
    }
}
