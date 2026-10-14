export interface Employee {
  id: number;
  fullName: string;
  email: string;
  jobTitle: string;
  department: string;
  country: string;
  salary: number;
  currency: string;
  joiningDate: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type SortDirection = 'asc' | 'desc';

export interface EmployeeQuery {
  search: string;
  country: string;
  department: string;
  jobTitle: string;
  page: number;
  size: number;
  sortBy: string;
  direction: SortDirection;
}
