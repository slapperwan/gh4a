/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a.adapter;


public class JobAdapter {

//    public JobAdapter(Context context, List<Job> objects) {
//        super(context, objects);
//    }
//    
//    @Override
//    public View doGetView(int position, View convertView, ViewGroup parent) {
//        View v = convertView;
//        ViewHolder viewHolder = null;
//
//        if (v == null) {
//            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
//            v = vi.inflate(R.layout.row_simple_3, null);
//            viewHolder = new ViewHolder();
//            viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
//            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
//            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
//            
//            v.setTag(viewHolder);
//        }
//        else {
//            viewHolder = (ViewHolder) v.getTag();
//        }
//
//        final Job job = mObjects.get(position);
//        if (job != null) {
//            viewHolder.tvTitle.setText(job.getTitle());
//            viewHolder.tvDesc.setText(job.getCompany());
//            viewHolder.tvExtra.setText((job.getType() != null ? job.getType().value() + " | " : "") 
//                    + job.getLocation() 
//                    + (job.getCreatedAt() != null ? " | " + pt.format(job.getCreatedAt()) : ""));
//        }
//        return v;
//    }
//
//    private static class ViewHolder {
//        public TextView tvTitle;
//        public TextView tvDesc;
//        public TextView tvExtra;
//
//    }
}
