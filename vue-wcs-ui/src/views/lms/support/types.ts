export type Reply = {
  id: string;
  res_id: string;
  support_id: string;
  lc_id: string;
  content: string;
  creator_id: string;
  updator_id?: string;
  creator_nm: string;
  is_deleted: boolean;
  created_at: string;
  updated_at?: string;
  domain_id: number;
}

export type OptionType = {
  label: string;
  value: string;
  key?: string;
};

export interface FileInfo {
  file_name: string;
  file_type: string;
  base_64_data: string;
  base64_data: string;
  size: number;
}

export const ALLOWED_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif', 'pdf', 'txt', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx']
