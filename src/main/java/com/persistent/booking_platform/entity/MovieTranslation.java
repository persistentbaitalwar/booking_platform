package com.persistent.booking_platform.entity;

import jakarta.persistence.*;

/** Localised title of a movie, e.g. the Hindi title for locale "hi". */
@Entity
@Table(name = "movie_translation",
        uniqueConstraints = @UniqueConstraint(name = "uk_movie_locale", columnNames = {"movie_id", "locale"}))
public class MovieTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @Column(nullable = false, length = 10)
    private String locale;

    @Column(nullable = false, length = 200)
    private String title;

    protected MovieTranslation() {
    }

    public MovieTranslation(Movie movie, String locale, String title) {
        this.movie = movie;
        this.locale = locale;
        this.title = title;
    }

    public Movie getMovie() { return movie; }
    public String getLocale() { return locale; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
