package br.com.tarefas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TarefaController {

    private static final Logger log = LoggerFactory.getLogger(TarefaController.class);

    private final JdbcTemplate jdbc;
    // No Kubernetes, a variavel HOSTNAME contem o nome do pod que esta atendendo
    private final String pod = System.getenv().getOrDefault("HOSTNAME", "local");

    public TarefaController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/info")
    public Map<String, String> info() {
        log.info("Requisicao /api/info atendida pelo pod {}", pod);
        return Map.of("pod", pod);
    }

    @GetMapping("/tarefas")
    public List<Map<String, Object>> listar() {
        log.info("Listando tarefas - pod {}", pod);
        return jdbc.queryForList(
                "SELECT id, titulo, concluida, pod, criada_em FROM tarefas ORDER BY id DESC");
    }

    @PostMapping("/tarefas")
    public ResponseEntity<Map<String, Object>> criar(@RequestBody Map<String, String> body) {
        String titulo = body.getOrDefault("titulo", "").trim();
        if (titulo.isEmpty()) {
            log.warn("Tentativa de criar tarefa sem titulo - pod {}", pod);
            return ResponseEntity.badRequest().body(Map.of("erro", "titulo obrigatorio"));
        }
        Integer id = jdbc.queryForObject(
                "INSERT INTO tarefas (titulo, pod) VALUES (?, ?) RETURNING id", Integer.class, titulo, pod);
        log.info("Tarefa {} criada: '{}' - pod {}", id, titulo, pod);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id, "titulo", titulo, "pod", pod));
    }

    @PutMapping("/tarefas/{id}/concluir")
    public ResponseEntity<Void> concluir(@PathVariable int id) {
        int linhas = jdbc.update("UPDATE tarefas SET concluida = NOT concluida WHERE id = ?", id);
        log.info("Tarefa {} alternou status - pod {}", id, pod);
        return linhas == 0 ? ResponseEntity.notFound().build() : ResponseEntity.noContent().build();
    }

    @DeleteMapping("/tarefas/{id}")
    public ResponseEntity<Void> excluir(@PathVariable int id) {
        int linhas = jdbc.update("DELETE FROM tarefas WHERE id = ?", id);
        log.info("Tarefa {} excluida - pod {}", id, pod);
        return linhas == 0 ? ResponseEntity.notFound().build() : ResponseEntity.noContent().build();
    }
}
