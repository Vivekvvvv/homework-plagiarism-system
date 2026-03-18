import http from "./http";

export type CoursePayload = {
  courseCode: string;
  courseName: string;
  teacherId: number;
  semester: string;
};

export const listCoursesApi = () => http.get("/courses");
export const createCourseApi = (payload: CoursePayload) => http.post("/courses", payload);
