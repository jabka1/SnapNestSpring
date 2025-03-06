package team.snapnestspring.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Album {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL)
    private List<Photo> photos;

    @ManyToOne
    @JoinColumn(name = "parent_album_id")
    private Album parentAlbum;

    @OneToMany(mappedBy = "parentAlbum", cascade = CascadeType.ALL)
    private List<Album> subAlbums = new ArrayList<>();

    public Album(String name, User user) {
        this.name = name;
        this.user = user;
    }
}
