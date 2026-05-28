select
	b.name,
	b.description,
	b.term ,
	b.col_size ,
	b.ref_type,
	b.sort_rank ,
	case
		when coalesce(b.grid_editor, 'hidden') = 'hidden' then true
		else false
	end if_show,
	b.grid_editor,
	b.grid_width ,
	coalesce(b.grid_align, 'left') as grid_align ,
	b.form_editor,
	b.ref_name,
	b.search_editor,
	b.search_oper,
	b.search_rank
from
	vue_menus a,
	menu_columns b
where
	a.name =:router
	and a.id = b.menu_id
	and b.grid_editor is not null
order by
	grid_rank