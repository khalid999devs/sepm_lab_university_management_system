CREATE TABLE courses (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code text NOT NULL UNIQUE CHECK (code = upper(btrim(code)) AND code ~ '^[A-Z][A-Z0-9 -]{1,19}$'),
    title text NOT NULL CHECK (char_length(btrim(title)) BETWEEN 2 AND 150),
    credits integer NOT NULL CHECK (credits BETWEEN 1 AND 6),
    capacity integer NOT NULL CHECK (capacity BETWEEN 1 AND 200),
    teacher_id bigint NOT NULL REFERENCES user_accounts(id)
);
CREATE INDEX courses_teacher_idx ON courses (teacher_id, id);

CREATE TABLE enrollments (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_id bigint NOT NULL REFERENCES courses(id),
    student_id bigint NOT NULL REFERENCES user_accounts(id),
    enrolled_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT enrollments_course_student_key UNIQUE (course_id, student_id)
);
CREATE INDEX enrollments_student_idx ON enrollments (student_id, id);

CREATE TABLE notices (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title text NOT NULL CHECK (char_length(btrim(title)) BETWEEN 2 AND 150),
    body text NOT NULL CHECK (char_length(btrim(body)) BETWEEN 2 AND 3000),
    published_at timestamptz NOT NULL DEFAULT now(),
    author_id bigint REFERENCES user_accounts(id) ON DELETE SET NULL
);
CREATE INDEX notices_published_idx ON notices (published_at DESC, id DESC);
CREATE INDEX notices_author_idx ON notices (author_id);
