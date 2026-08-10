// package com.example.demo.infraestructure.leader;

// import java.io.IOException;
// import java.nio.charset.StandardCharsets;

// import org.springframework.core.io.Resource;
// import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
// import org.springframework.stereotype.Component;

// @Component
// public class PlaybookLoader {

//     private final String playbooks;

//     public PlaybookLoader() throws IOException {
//         StringBuilder sb = new StringBuilder();
//         var resources = new PathMatchingResourcePatternResolver()
//                 .getResources("classpath:playbooks/*.md");
//         for (Resource r : resources) {
//             sb.append(r.getContentAsString(StandardCharsets.UTF_8)).append("\n\n");
//         }
//         this.playbooks = sb.toString();
//     }

//     public String get() { return playbooks; }
// }