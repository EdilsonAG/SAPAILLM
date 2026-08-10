package com.example.demo.infraestructure.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.domain.dto.CriarTicketStatus;
import com.example.demo.domain.dto.ResponseIA;
import com.example.demo.domain.service.TriagemAgentService;
//import com.example.demo.infraestructure.leader.PlaybookLoader;

 
@RestController
@RequestMapping("/chat")
public class ChatController {
    private final TriagemAgentService agent;
 
    public ChatController(TriagemAgentService agent) {
         this.agent = agent; 
          }

    @PostMapping
    public ResponseIA chat(@RequestBody ChatRequest req) {
        System.out.println("chegou");
            ResponseIA r = agent.chat(req.session(), req.message());

         if (r.status() != CriarTicketStatus.PRONTO) {
            return new ResponseIA(r.resposta(), r.status(), r.description(), r.longText(), null, null);
        }
        return r;
    }
    public record ChatRequest(String message, String session) {}

}
