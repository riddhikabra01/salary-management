// Shape of an employee as the API returns it. Dates arrive as ISO strings (yyyy-MM-dd).
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

// What we send when creating or editing: the same fields minus the server-generated id.
export type EmployeeRequest = Omit<Employee, 'id'>;

// Generic wrapper for one page of results, plus the totals the paginator needs.
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type SortDirection = 'asc' | 'desc';

// Everything the list screen can ask for. Secondary sort is optional;
// leaving it out means "just the primary sort, then name".
export interface EmployeeQuery {
  search: string;
  country: string;
  department: string;
  jobTitle: string;
  page: number;
  size: number;
  sortBy: string;
  direction: SortDirection;
  secondarySortBy?: string;
  secondaryDirection?: SortDirection;
}
