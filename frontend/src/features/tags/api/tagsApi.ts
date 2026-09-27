import { httpClient } from '@/shared/api/httpClient'
import type { Tag, TagInput } from '../types'

export const tagsApi = {
  list: () => httpClient.get<Tag[]>('/tags'),
  create: (input: TagInput) => httpClient.post<Tag, TagInput>('/tags', input),
  update: (id: number, input: TagInput) => httpClient.put<Tag, TagInput>(`/tags/${id}`, input),
  remove: (id: number) => httpClient.delete(`/tags/${id}`),
}
