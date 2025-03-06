package team.snapnestspring.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Data
@Setter
@Getter
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private boolean isPublic;

    @ManyToOne
    @JoinColumn(name = "album_id", nullable = true)
    private Album album;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
