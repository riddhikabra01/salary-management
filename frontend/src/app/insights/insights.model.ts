export interface FilterOptions {
  countries: string[];
  departments: string[];
  jobTitles: string[];
}

export interface CountrySalaryStats {
  country: string;
  currency: string;
  headcount: number;
  minSalary: number;
  maxSalary: number;
  averageSalary: number;
}

export interface JobTitleSalaryStats {
  country: string;
  jobTitle: string;
  currency: string;
  headcount: number;
  averageSalary: number;
}

export interface SalaryBand {
  from: number;
  to: number;
  headcount: number;
}

export interface SalaryDistribution {
  country: string;
  currency: string;
  bands: SalaryBand[];
}
