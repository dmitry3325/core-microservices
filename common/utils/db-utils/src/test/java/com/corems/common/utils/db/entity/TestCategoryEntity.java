package com.corems.common.utils.db.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "test_category")
public class TestCategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String code;

    public TestCategoryEntity() {}

    public TestCategoryEntity(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}

